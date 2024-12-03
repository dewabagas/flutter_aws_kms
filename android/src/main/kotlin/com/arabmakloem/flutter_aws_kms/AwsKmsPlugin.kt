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
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CachedCredentialsProvider

class AwsKmsPlugin : FlutterPlugin {
    private lateinit var channel: MethodChannel

    private var credentialsProvider: CredentialsProvider? = null
    private var region: String? = null
    private var keyId: String? = null
    private var accessKeyId: String? = null
    private var secretAccessKey: String? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "aws.kms.channel")
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "configure" -> {
                    accessKeyId = call.argument<String>("accessKeyId")
                    secretAccessKey = call.argument<String>("secretAccessKey")
                    region = call.argument<String>("region")
                    keyId = call.argument<String>("keyId")

                    if (accessKeyId != null && secretAccessKey != null && region != null && keyId != null) {
                        // credentialsProvider = createCredentialsProvider(accessKeyId, secretAccessKey)
                        result.success("AWS KMS configured successfully")
                    } else {
                        result.error("CONFIGURATION_ERROR", "Invalid configuration parameters", null)
                    }
                }
                "encrypt" -> {
                    val plaintext = call.argument<String>("plaintext")
                    if (credentialsProvider != null && region != null && keyId != null && plaintext != null) {
                        GlobalScope.launch {
                            val encryptedData = encryptData(plaintext)
                            withContext(Dispatchers.Main) {
                                if (encryptedData != null) {
                                    result.success(encryptedData)
                                } else {
                                    result.error("ENCRYPTION_ERROR", "Failed to encrypt data", null)
                                }
                            }
                        }
                    } else {
                        result.error("INVALID_ARGUMENTS", "Missing configuration or arguments", null)
                    }
                }
                "decrypt" -> {
                    val encryptedText = call.argument<String>("encryptedText")
                    if (credentialsProvider != null && region != null && keyId != null && encryptedText != null) {
                        GlobalScope.launch {
                            val decryptedData = decryptData(encryptedText)
                            withContext(Dispatchers.Main) {
                                if (decryptedData != null) {
                                    result.success(decryptedData)
                                } else {
                                    result.error("DECRYPTION_ERROR", "Failed to decrypt data", null)
                                }
                            }
                        }
                    } else {
                        result.error("INVALID_ARGUMENTS", "Missing configuration or arguments", null)
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

    private suspend fun encryptData(plaintext: String): String? {
        return withContext(Dispatchers.IO) {
            val kmsClient = KmsClient {
                this.region = region
                this.credentialsProvider = createCredentialsProvider(accessKeyId!!, secretAccessKey!!)
            }
            try {
                val encryptRequest = EncryptRequest {
                    this.keyId = keyId
                    this.plaintext = plaintext.toByteArray()
                }
                println("EncryptRequest KeyId: $keyId")
                println("EncryptRequest Plaintext: $plaintext")
    
                val response = kmsClient.encrypt(encryptRequest)
                println("Encryption successful. CiphertextBlob: ${response.ciphertextBlob}")
    
                Base64.getEncoder().encodeToString(response.ciphertextBlob)
            } catch (e: Exception) {
                println("Error during encryption: ${e.message}")
                e.printStackTrace()
                null
            } finally {
                println("Closing KMS Client")
                kmsClient.close()
            }
        }
    }
    
    
    

    private suspend fun decryptData(encryptedText: String): String? {
        return withContext(Dispatchers.IO) {
            val kmsClient = KmsClient {
                this.region = region
                this.credentialsProvider = createCredentialsProvider(accessKeyId!!, secretAccessKey!!)
            }
            try {
                val encryptedBytes = Base64.getDecoder().decode(encryptedText)
                val decryptRequest = DecryptRequest {
                    this.ciphertextBlob = encryptedBytes
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
