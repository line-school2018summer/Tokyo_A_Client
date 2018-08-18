package intern.line.tokyoaclient.HttpConnection

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import intern.line.tokyoaclient.HttpConnection.service.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


val base_url = "http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com"

val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()
val retrofit = Retrofit.Builder()
        .baseUrl(base_url)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build()

val userProfileService: UserProfileService = retrofit.create(UserProfileService::class.java)
val talkService: TalkService = retrofit.create(TalkService::class.java)