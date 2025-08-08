package com.hussain.service;

import java.util.List;

import org.json.JSONObject;
import com.hussain.model.Prompt;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hussain.constant.Constants;
import software.amazon.awssdk.core.SdkBytes;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamResponseHandler;

@Slf4j
@Service
@AllArgsConstructor
public class AssistantService implements Constants {

    private final BedrockRuntimeClient bedrockClient;

    private final BedrockRuntimeAsyncClient bedrockAsyncClient;

    public String askAssistant(Prompt prompt) {
        String enclosedPrompt = "Human: " + prompt.getQuestion() + "\n\nAssistant:";
        if (prompt.getResponseType().equals("async")) {
            return asyncResponse(enclosedPrompt);
        } else if (prompt.getResponseType().equals("sync")) {
            return syncResponse(enclosedPrompt);
        }
        return "";
    }

    /*
     * * SYNCHRONOUS CALL TO AI FOR TEXT RESPONSE
     */
    private String syncResponse(String enclosedPrompt) {
        String payload = new JSONObject().put("prompt", enclosedPrompt)
                .put("max_tokens_to_sample", 200)
                .put("temperature", 0.5)
                .put("stop_sequences", List.of("\n\nHuman:")).toString();
        InvokeModelRequest request = InvokeModelRequest.builder().body(SdkBytes.fromUtf8String(payload))
                .modelId(ANTHROPIC_CLAUDE_V_2).contentType(APPLICATION_JSON).accept(APPLICATION_JSON).build();
        InvokeModelResponse response = bedrockClient.invokeModel(request);
        JSONObject responseBody = new JSONObject(response.body().asUtf8String());
        log.info("Response from model : {}", responseBody);
        String generatedText = responseBody.getString("completion");
        log.info("Generated text: " + generatedText);
        return generatedText;
    }

    /*
     * * ASYNCHRONOUS CALL TO AI FOR TEXT RESPONSE
     */
    private String asyncResponse(String enclosedPrompt) {
        var finalCompletion = new AtomicReference<>("");
        var silent = false;
        var payload = new JSONObject().put("prompt", enclosedPrompt).put("temperature", 0.8).put("max_tokens_to_sample", 300).toString();
        var request = InvokeModelWithResponseStreamRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(ANTHROPIC_CLAUDE_V_2).contentType(APPLICATION_JSON).accept(APPLICATION_JSON).build();
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
        bedrockAsyncClient.invokeModelWithResponseStream(request, handler).join();
        return finalCompletion.get();
    }
}
