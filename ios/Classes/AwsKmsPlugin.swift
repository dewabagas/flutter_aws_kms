import Flutter
import UIKit
import AWSKMS
import AWSClientRuntime

public class AwsKmsPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "aws.kms.channel", binaryMessenger: registrar.messenger())
        let instance = AwsKmsPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "encrypt":
            guard let args = call.arguments as? [String: Any],
                  let accessKeyId = args["accessKeyId"] as? String,
                  let secretAccessKey = args["secretAccessKey"] as? String,
                  let region = args["region"] as? String,
                  let keyId = args["keyId"] as? String,
                  let plaintext = args["plaintext"] as? String else {
                result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing arguments", details: nil))
                return
            }
            encryptData(accessKeyId: accessKeyId, secretAccessKey: secretAccessKey, region: region, keyId: keyId, plaintext: plaintext, result: result)

        case "decrypt":
            guard let args = call.arguments as? [String: Any],
                  let accessKeyId = args["accessKeyId"] as? String,
                  let secretAccessKey = args["secretAccessKey"] as? String,
                  let region = args["region"] as? String,
                  let keyId = args["keyId"] as? String,
                  let encryptedText = args["encryptedText"] as? String else {
                result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing arguments", details: nil))
                return
            }
            decryptData(accessKeyId: accessKeyId, secretAccessKey: secretAccessKey, region: region, keyId: keyId, encryptedText: encryptedText, result: result)

        default:
            result(FlutterMethodNotImplemented)
        }
    }

    private func createKMSClient(accessKeyId: String, secretAccessKey: String, region: String) throws -> KMSClient {
        let config = try KMSClient.KMSClientConfiguration(
            region: region
        )
        return KMSClient(config: config)
    }

    private func encryptData(accessKeyId: String, secretAccessKey: String, region: String, keyId: String, plaintext: String, result: @escaping FlutterResult) {
        Task {
            do {
                let kmsClient = try createKMSClient(accessKeyId: accessKeyId, secretAccessKey: secretAccessKey, region: region)
                let request = EncryptInput(keyId: keyId, plaintext: plaintext.data(using: .utf8)!)
                let response = try await kmsClient.encrypt(input: request)

                if let ciphertextBlob = response.ciphertextBlob {
                    let encryptedText = ciphertextBlob.base64EncodedString()
                    result(encryptedText)
                } else {
                    result(FlutterError(code: "ENCRYPTION_ERROR", message: "No encrypted data returned", details: nil))
                }
            } catch {
                result(FlutterError(code: "ENCRYPTION_ERROR", message: "Failed to encrypt data", details: error.localizedDescription))
            }
        }
    }

    private func decryptData(accessKeyId: String, secretAccessKey: String, region: String, keyId: String, encryptedText: String, result: @escaping FlutterResult) {
        Task {
            do {
                let kmsClient = try createKMSClient(accessKeyId: accessKeyId, secretAccessKey: secretAccessKey, region: region)
                guard let ciphertextBlob = Data(base64Encoded: encryptedText) else {
                    result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid base64 encoded ciphertext", details: nil))
                    return
                }

                let request = DecryptInput(ciphertextBlob: ciphertextBlob, keyId: keyId)
                let response = try await kmsClient.decrypt(input: request)

                if let plaintext = response.plaintext {
                    let decryptedText = String(data: plaintext, encoding: .utf8)
                    result(decryptedText)
                } else {
                    result(FlutterError(code: "DECRYPTION_ERROR", message: "No decrypted data returned", details: nil))
                }
            } catch {
                result(FlutterError(code: "DECRYPTION_ERROR", message: "Failed to decrypt data", details: error.localizedDescription))
            }
        }
    }
}
