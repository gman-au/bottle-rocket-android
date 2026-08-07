package au.com.gman.bottlerocket.activity

import android.content.Intent
import android.os.Bundle
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.contracts.WorkflowSummary
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : WorkflowLoadingActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        loadCachesAndProceed()
    }

    override fun onWorkflowsLoaded(workflowSummaries: List<WorkflowSummary>) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}