package au.com.gman.bottlerocket.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.interfaces.IWorkflowCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var workflowCache: IWorkflowCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super
            .onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        loadCachesAndProceed()
    }

    private fun loadCachesAndProceed() {
        lifecycleScope.launch {
            val result = workflowCache.loadWorkflows()

            result.fold(
                onSuccess = { templates ->
                    startMainActivity()
                },
                onFailure = { error ->
                    startServerWarningActivity()
                }
            )
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun startServerWarningActivity() {
        val intent = Intent(this, ServerWarningActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}