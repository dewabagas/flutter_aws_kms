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

    private lateinit var credentialsProvider: CredentialsProvider
    private lateinit var region: String
    private lateinit var keyId: String
    private lateinit var accessKeyId: String
    private lateinit var secretAccessKey: String

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "aws.kms.channel")
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "configure" -> {
                    val tempAccessKeyId = call.argument<String>("accessKeyId")
                    val tempSecretAccessKey = call.argument<String>("secretAccessKey")
                    val tempRegion = call.argument<String>("region")
                    val tempKeyId = call.argument<String>("keyId")

                    if (tempAccessKeyId != null && tempSecretAccessKey != null && 
                        tempRegion != null && tempKeyId != null) {
                        
                        accessKeyId = tempAccessKeyId
                        secretAccessKey = tempSecretAccessKey
                        region = tempRegion
                        keyId = tempKeyId
                        
                        result.success("AWS KMS configured successfully")
                    } else {
                        result.error(
                            "CONFIGURATION_ERROR", 
                            "Invalid configuration parameters", 
                            null
                        )
                    }  
                }
                "encrypt" -> {
                    val plaintext = call.argument<String>("plaintext")
                    if (region != null && keyId != null && plaintext != null) {
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
                    if (region != null && keyId != null && encryptedText != null) {
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

    private fun isConfigured(): Boolean {
        return try {
            accessKeyId
            secretAccessKey
            region
            keyId
            true
        } catch (e: UninitializedPropertyAccessException) {
            false
        }
    }

    private suspend fun encryptData(plaintext: String): String? {
        if (!isConfigured()) {
            println("Error: AWS KMS not properly configured")
            return null
        }
        
        return withContext(Dispatchers.IO) {
            val kmsClient = KmsClient {
                this.region = this@AwsKmsPlugin.region
                credentialsProvider = createCredentialsProvider(accessKeyId, secretAccessKey)
            }
    
            try {
                val encryptRequest = EncryptRequest {
                    this.keyId = this@AwsKmsPlugin.keyId
                    this.plaintext = plaintext.toByteArray()
                }
                
                val response = kmsClient.encrypt(encryptRequest)
                Base64.getEncoder().encodeToString(response.ciphertextBlob)
            } catch (e: Exception) {
                println("Error during encryption: ${e.message}")
                e.printStackTrace()
                null
            } finally {
                kmsClient.close()
            }
        }
    }

    private suspend fun decryptData(encryptedText: String): String? {
        if (!isConfigured()) {
            println("Error: AWS KMS not properly configured")
            return null
        }
        return withContext(Dispatchers.IO) {
            val kmsClient = KmsClient {
                this.region = this@AwsKmsPlugin.region
                credentialsProvider = createCredentialsProvider(accessKeyId, secretAccessKey)
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
