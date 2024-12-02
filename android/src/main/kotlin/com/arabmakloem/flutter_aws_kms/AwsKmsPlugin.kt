package com.arabmakloem.flutter_aws_kms

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodChannel
import aws.sdk.kotlin.services.kms.KmsClient
import aws.sdk.kotlin.services.kms.model.DecryptRequest
import aws.sdk.kotlin.services.kms.model.EncryptRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Base64
import aws.smithy.kotlin.runtime.auth.awscredentials.CachedCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider

class AwsKmsPlugin : FlutterPlugin {
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "aws.kms.channel")
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "encrypt" -> {
                    val accessKeyId = call.argument<String>("accessKeyId")
                    val secretAccessKey = call.argument<String>("secretAccessKey")
                    val region = call.argument<String>("region")
                    val keyId = call.argument<String>("keyId")
                    val plaintext = call.argument<String>("plaintext")
                    if (accessKeyId != null && secretAccessKey != null && region != null && keyId != null && plaintext != null) {
                        GlobalScope.launch {
                            val encryptedData = encryptData(accessKeyId, secretAccessKey, region, keyId, plaintext)
                            withContext(Dispatchers.Main) {
                                if (encryptedData != null) {
                                    result.success(encryptedData)
                                } else {
                                    result.error("ENCRYPTION_ERROR", "Failed to encrypt data", null)
                                }
                            }
                        }
                    } else {
                        result.error("INVALID_ARGUMENTS", "Missing arguments", null)
                    }
                }
                "decrypt" -> {
                    val accessKeyId = call.argument<String>("accessKeyId")
                    val secretAccessKey = call.argument<String>("secretAccessKey")
                    val region = call.argument<String>("region")
                    val keyId = call.argument<String>("keyId")
                    val encryptedText = call.argument<String>("encryptedText")
                    if (accessKeyId != null && secretAccessKey != null && region != null && keyId != null && encryptedText != null) {
                        GlobalScope.launch {
                            val decryptedData = decryptData(accessKeyId, secretAccessKey, region, keyId, encryptedText)
                            withContext(Dispatchers.Main) {
                                if (decryptedData != null) {
                                    result.success(decryptedData)
                                } else {
                                    result.error("DECRYPTION_ERROR", "Failed to decrypt data", null)
                                }
                            }
                        }
                    } else {
                        result.error("INVALID_ARGUMENTS", "Missing arguments", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun createCredentialsProvider(accessKeyId: String, secretAccessKey: String): CredentialsProvider {
        return CachedCredentialsProvider(
            StaticCredentialsProvider(
                Credentials(
                    accessKeyId = accessKeyId,
                    secretAccessKey = secretAccessKey
                )
            )
        )
    }

    private suspend fun encryptData(
        accessKeyId: String,
        secretAccessKey: String,
        region: String,
        keyId: String,
        plaintext: String
    ): String? {
        return withContext(Dispatchers.IO) {
            val kmsClient = KmsClient {
                this.region = region
                credentialsProvider = createCredentialsProvider(accessKeyId, secretAccessKey)
            }
            try {
                val encryptRequest = EncryptRequest {
                    this.keyId = keyId
                    this.plaintext = plaintext.toByteArray()
                }
                val response = kmsClient.encrypt(encryptRequest)
                Base64.getEncoder().encodeToString(response.ciphertextBlob)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                kmsClient.close()
            }
        }
    }

    private suspend fun decryptData(
        accessKeyId: String,
        secretAccessKey: String,
        region: String,
        keyId: String,
        encryptedText: String
    ): String? {
        return withContext(Dispatchers.IO) {
            val kmsClient = KmsClient {
                this.region = region
                credentialsProvider = createCredentialsProvider(accessKeyId, secretAccessKey)
            }
            try {
                val encryptedBytes = Base64.getDecoder().decode(encryptedText)
                val decryptRequest = DecryptRequest {
                    this.ciphertextBlob = encryptedBytes
                    this.keyId = keyId
                }
                val response = kmsClient.decrypt(decryptRequest)
                String(response.plaintext!!)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                kmsClient.close()
            }
        }
    }
}
