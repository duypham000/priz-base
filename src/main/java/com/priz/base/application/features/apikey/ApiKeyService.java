package com.priz.base.application.features.apikey;

import com.priz.base.application.features.apikey.dto.CreateApiKeyRequest;
import com.priz.base.application.features.apikey.dto.CreateApiKeyResponse;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyCredentialsModel;

import java.util.List;

public interface ApiKeyService {

    CreateApiKeyResponse create(CreateApiKeyRequest request);

    CreateApiKeyResponse regenerate(String id);

    void revoke(String id);

    ApiKeyCredentialsModel getById(String id);

    List<ApiKeyCredentialsModel> getAll();
}
