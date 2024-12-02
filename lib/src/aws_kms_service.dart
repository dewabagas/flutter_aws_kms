import 'dart:developer';
import 'package:flutter/services.dart';

class AwsKmsService {
  static const _channel = MethodChannel('aws.kms.channel');

  Future<String?> encrypt(String accessKeyId, String secretAccessKey,
      String region, String keyId, String plaintext) async {
    try {
      final encryptedText = await _channel.invokeMethod<String>(
        'encrypt',
        {
          'accessKeyId': accessKeyId,
          'secretAccessKey': secretAccessKey,
          'region': region,
          'keyId': keyId,
          'plaintext': plaintext,
        },
      );
      return encryptedText;
    } catch (e) {
      log('Error during encryption: $e');
      return null;
    }
  }

  Future<String?> decrypt(String accessKeyId, String secretAccessKey,
      String region, String keyId, String encryptedText) async {
    try {
      final decryptedText = await _channel.invokeMethod<String>(
        'decrypt',
        {
          'accessKeyId': accessKeyId,
          'secretAccessKey': secretAccessKey,
          'region': region,
          'keyId': keyId,
          'encryptedText': encryptedText,
        },
      );
      return decryptedText;
    } catch (e) {
      log('Error during decryption: $e');
      return null;
    }
  }
}
