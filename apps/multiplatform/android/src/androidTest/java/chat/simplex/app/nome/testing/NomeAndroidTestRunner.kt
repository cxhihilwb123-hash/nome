package chat.simplex.app.nome.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class NomeAndroidTestRunner : AndroidJUnitRunner() {
  override fun newApplication(
    cl: ClassLoader,
    className: String,
    context: Context,
  ): Application {
    System.setProperty("im.nome.app.instrumentation_process", "true")
    return super.newApplication(cl, className, context)
  }
}
