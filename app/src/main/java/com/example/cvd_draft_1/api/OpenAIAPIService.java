package com.example.cvd_draft_1.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface OpenAIAPIService {
    @Headers({
            "Content-Type: application/json",
            "Authorization: Bearer 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"
    })
    @POST("v1/chat/completions")  // Updated to the correct endpoint for chat models
    Call<OpenAIResponse> createCompletion(@Body OpenAIRequest body);
}