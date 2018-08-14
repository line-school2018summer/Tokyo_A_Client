package intern.line.tokyoaclient.HttpConnection

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


val base_url = "http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com"

lateinit var retrofit: Retrofit

val service: IApiService = create(IApiService::class.java)

fun <S> create(serviceClass: Class<S>): S {
    val gson = GsonBuilder()
            .serializeNulls()
            .create()

    // create retrofit
    retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(base_url)
            .client(httpBuilder.build())
            .build()

    return retrofit.create(serviceClass)
}
