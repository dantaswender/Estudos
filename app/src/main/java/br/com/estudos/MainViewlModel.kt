package br.com.estudos

import android.util.Log
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Provider {
    fun provider(): MainUseCase = MainUseCaseImpl(RepositoryImpl())
    fun providerApi(): EstudosServiceApi = AppRetrofit.service
}

class MainViewlModel : ViewModel() {

    private val useCase by lazy {
        Provider().provider()
    }

    init {
        Log.d("###", "init viewModel")

    }

    fun getDados() {
        useCase.getDados()
    }
}

interface MainUseCase {
    fun getDados()
}

class MainUseCaseImpl(val repository: Repository) : MainUseCase {
    override fun getDados() {
        repository.getDados()
    }

}

interface Repository {
    fun getDados()

}

data class Pessoa(val nome: String)

class RepositoryImpl : Repository {
    override fun getDados() {

        Provider().providerApi().getDados().enqueue(
            object : Callback<ResponseDados> {
                override fun onResponse(
                    call: Call<ResponseDados>,
                    response: Response<ResponseDados>
                ) {
                    Log.d("###", "${response.body()?.mData?.totalCases}")
                }

                override fun onFailure(call: Call<ResponseDados>, t: Throwable) {
                    Log.d("###", "$call $t")
                }

            }
        )

        getWithRx()
    }

    private fun getWithRx() {

        Provider().providerApi().getDados2()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ResponseDados> {
                override fun onSubscribe(d: Disposable) {
                    Log.d("###", "onSubscribe")
                }

                override fun onNext(t: ResponseDados) {
                    Log.d("###", "RxJava ${t?.mData?.totalCases}")
                }

                override fun onError(e: Throwable) {
                    Log.d("###", "onError")
                }

                override fun onComplete() {
                    Log.d("###", "onComplete")
                }

            })

    }

}