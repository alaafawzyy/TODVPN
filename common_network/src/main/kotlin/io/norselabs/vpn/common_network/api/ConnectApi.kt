package io.norselabs.vpn.common_network.api

import io.norselabs.vpn.common_network.models.CreateCredentials
import io.norselabs.vpn.common_network.models.CredentialsResponse
import io.norselabs.vpn.common_network.models.DataObj
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ConnectApi {

  @POST("city/{cityId}/credentials")
  suspend fun getCredentials(
    @Path("cityId") cityId: String,
    @Body body: CreateCredentials,
  ): DataObj<CredentialsResponse>
}
