package br.com.estudos

import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.GET

interface EstudosServiceApi {

    @GET("cases/general-stats")
    fun getDados() : Call<ResponseDados>

    @GET("cases/general-stats")
    fun getDados2() : Observable<ResponseDados>
}