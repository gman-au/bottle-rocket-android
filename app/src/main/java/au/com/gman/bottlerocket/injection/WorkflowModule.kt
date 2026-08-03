package au.com.gman.bottlerocket.injection
import au.com.gman.bottlerocket.interfaces.IWorkflowCache
import au.com.gman.bottlerocket.workflows.WorkflowCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkflowModule {
    @Singleton
    @Binds
    abstract fun bindWorkflowCache(
        workflowCache: WorkflowCache
    ): IWorkflowCache
}