package com.youshu.app.di

import com.youshu.app.data.warehouse.WorkerWarehouseRecognitionGateway
import com.youshu.app.data.warehouse.WarehouseRecognitionGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WarehouseRecognitionModule {
    @Binds
    abstract fun bindGateway(implementation: WorkerWarehouseRecognitionGateway): WarehouseRecognitionGateway
}
