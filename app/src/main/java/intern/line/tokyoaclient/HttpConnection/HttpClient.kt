package intern.line.tokyoaclient.HttpConnection

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

val httpBuilder: OkHttpClient.Builder get() {
    // create http client
    val httpClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()

                // header
                val request = original.newBuilder()
                        .header("Accept", "application/json")
                        .method(original.method(), original.body())
                        .build()

                return@Interceptor chain.proceed(request)
            })
            .readTimeout(30, TimeUnit.SECONDS)

    // log interceptor
    /*
    val loggingInInterceptor = HttpLoggingInterceptor()
    loggingInInterceptor.level = HttpLoggingInterceptor.Level.BODY
    httpClient.addInterceptor(loggingInInterceptor)
    */
    return httpClient
}