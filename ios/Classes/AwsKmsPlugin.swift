import Flutter
import UIKit
import AWSKMS

public class AwsKmsPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "aws.kms.channel", binaryMessenger: registrar.messenger())
        let instance = AwsKmsPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    var accessKeyId: String?
    var secretAccessKey: String?
    var keyId: String?
    

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "configure":
            guard let args = call.arguments as? [String: Any],
                  let accessKeyIdData = args["accessKeyId"] as? String,
                  let secretAccessKeyData = args["secretAccessKey"] as? String,
                  let keyIdData = args["keyId"] as? String else {
                    result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing arguments", details: nil))
                    return
            }
            accessKeyId = accessKeyIdData
            secretAccessKey = secretAccessKeyData
            keyId = keyIdData
            
            configureAWS(accessKey: accessKeyId!, secretKey: secretAccessKey!, result: result)
        case "encrypt":
            guard let args = call.arguments as? [String: Any],
                  let plaintext = args["plaintext"] as? String else {
                result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing arguments", details: nil))
                return
            }
            encryptData(keyId: keyId!, plainText: plaintext, result: result)

        case "decrypt":
            guard let args = call.arguments as? [String: Any],
                  let encryptedText = args["encryptedText"] as? String else {
                result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing arguments", details: nil))
                return
            }
            decryptData(encryptedString: encryptedText, result: result)

        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    func configureAWS(accessKey: String, secretKey: String, result: FlutterResult) {
        let credentialsProvider = AWSStaticCredentialsProvider(accessKey: accessKey, secretKey: secretKey)
        let configuration = AWSServiceConfiguration(
            region: .USEast1,  // Set to the AWS region you are using
            credentialsProvider: credentialsProvider
        )
        AWSServiceManager.default().defaultServiceConfiguration = configuration
    }


    func encryptData(keyId: String, plainText: String, result: @escaping FlutterResult) {
        let kmsClient = AWSKMS.default()
        
        let encryptRequest = AWSKMSEncryptRequest()!
        encryptRequest.keyId = keyId // The KMS Key ID you want to use for encryption
        encryptRequest.plaintext = plainText.data(using: .utf8)
        
        kmsClient.encrypt(encryptRequest) { (response, error) in
            if let error = error {
                print("Error encrypting data: \(error.localizedDescription)")
                result(FlutterError(code: "INVALID_ARGUMENTS", message: error.localizedDescription, details: nil))
            }
            
            if let encryptedData = response?.ciphertextBlob {
                // Convert the encrypted data to a Base64-encoded string
                let base64EncryptedString = encryptedData.base64EncodedString()
                print("Encrypted data: \(base64EncryptedString)")
                result(base64EncryptedString)
            }
        }
        return
    }
    
    func decryptData(encryptedString: String, result: @escaping FlutterResult) {
        let kmsClient = AWSKMS.default()
        
        // Convert the Base64-encoded string back to Data
        guard let encryptedData = Data(base64Encoded: encryptedString) else {
            print("Error: Invalid Base64 string")
            result(FlutterError(code: "INVALID_BASE64_STRING", message: "Invalid Base64 string", details: nil))
            return
        }

        let decryptRequest = AWSKMSDecryptRequest()!
        decryptRequest.ciphertextBlob = encryptedData
        
        kmsClient.decrypt(decryptRequest) { (response, error) in
            if let error = error {
                print("Error decrypting data: \(error.localizedDescription)")
                result(FlutterError(code: "INVALID_ARGUMENTS", message: error.localizedDescription, details: nil))
            }
            
            if let decryptedData = response?.plaintext {
                if let decryptedString = String(data: decryptedData, encoding: .utf8) {
                    print("Decrypted string: \(decryptedString)")
                    result(decryptedString)
                }
            }
        }
        return
    }
}
