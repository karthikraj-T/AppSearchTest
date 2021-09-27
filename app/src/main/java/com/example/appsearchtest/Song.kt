package com.example.appsearchtest

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema

@Document
data class Song(
    @Document.Namespace
    val nameSpace: String,
    @Document.Id
    val id: String,
    @Document.Score
    val rate: Int,

    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val song: String,

    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val singer: String
)