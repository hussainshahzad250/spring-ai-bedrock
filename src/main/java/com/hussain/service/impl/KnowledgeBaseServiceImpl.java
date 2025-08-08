package com.hussain.service.impl;

import com.mongodb.client.AggregateIterable;
import org.bson.Document;
import com.hussain.constant.Constants;
import com.hussain.model.KnowledgeBase;
import com.hussain.model.Prompt;
import com.hussain.repository.KnowledgeBaseRepository;
import com.hussain.service.KnowledgeBaseService;
import com.hussain.service.KnowledgeBaseVectorSearch;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamResponseHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@AllArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService, Constants {

    private final BedrockRuntimeClient bedrockClient;

    private final BedrockRuntimeAsyncClient bedrockAsyncClient;

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    private final KnowledgeBaseVectorSearch knowledgeBaseVectorSearch;

    private static List<Double> jsonArrayToList(JSONArray jsonArray) {
        List<Double> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getDouble(i));
        }
        return list;
    }

    /*
     * Saving embeddings into database
     */
    @Override
    public String saveEmbeddings(Prompt prompt) {
        String payload = new JSONObject().put("inputText", prompt.getQuestion()).toString();
        InvokeModelRequest request = InvokeModelRequest.builder().body(SdkBytes.fromUtf8String(payload)).modelId(AMAZON_TITAN_EMBED_TEXT_V_1)
                .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).build();
        InvokeModelResponse response = bedrockClient.invokeModel(request);
        JSONObject responseBody = new JSONObject(response.body().asUtf8String());
        List<Double> vectorData = jsonArrayToList(responseBody.getJSONArray("embedding"));
        vectorData.forEach(System.out::println);

        KnowledgeBase data = new KnowledgeBase();
        data.setTextData(prompt.getQuestion());
        data.setVectorData(vectorData);
        knowledgeBaseRepository.save(data);
        return "Embeddings saved to database...!";
    }

    @Override
    public String askExpertAssistant(Prompt prompt) {
        /**
         * 1. Convert prompt to embeddings and fetch relevant content from vector database
         */
        String payload = new JSONObject().put("inputText", prompt.getQuestion()).toString();
        InvokeModelRequest request = InvokeModelRequest.builder().body(SdkBytes.fromUtf8String(payload)).modelId(AMAZON_TITAN_EMBED_TEXT_V_1)
                .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).build();
        InvokeModelResponse response = bedrockClient.invokeModel(request);
        JSONObject responseBody = new JSONObject(response.body().asUtf8String());

        List<Double> vectorQuery = jsonArrayToList(responseBody.getJSONArray("embedding"));

        /* 2. Query vector database */
        AggregateIterable<Document> context = knowledgeBaseVectorSearch.findByVectorData(vectorQuery);

        /* 3. Return relevant content */
        String enclosedPrompt = "Human:\n\n" + prompt.getQuestion();
        for (Document document : context) {
            enclosedPrompt = enclosedPrompt + "<context>" + document.getString("text_data") + "</context>\n";
        }
        enclosedPrompt = enclosedPrompt + "\n\n Assistant:";

        System.out.println(enclosedPrompt);

        /* 4. Generate response using Context */
        var finalCompletion = new AtomicReference<>("");
        var silent = false;
        var queryPayload = new JSONObject().put("prompt", enclosedPrompt).put("temperature", 0.0)
                .put("max_tokens_to_sample", 200).toString();
        var queryRequest = InvokeModelWithResponseStreamRequest.builder().body(SdkBytes.fromUtf8String(queryPayload))
                .modelId(ANTHROPIC_CLAUDE_V_2).contentType("application/json").accept("application/json").build();
        var visitor = InvokeModelWithResponseStreamResponseHandler.Visitor.builder().onChunk(chunk -> {
            var json = new JSONObject(chunk.bytes().asUtf8String());
            var completion = json.getString("completion");
            finalCompletion.set(finalCompletion.get() + completion);
            if (!silent) {
                System.out.print(completion);
            }
        }).build();
        var handler = InvokeModelWithResponseStreamResponseHandler.builder()
                .onEventStream(stream -> stream.subscribe(event -> event.accept(visitor))).onComplete(() -> {
                }).onError(e -> System.out.println("\n\nError: " + e.getMessage())).build();
        bedrockAsyncClient.invokeModelWithResponseStream(queryRequest, handler).join();
        return finalCompletion.get();
    }

}
