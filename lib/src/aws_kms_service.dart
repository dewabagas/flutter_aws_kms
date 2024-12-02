import 'dart:developer';
import 'package:flutter/services.dart';

class AwsKmsService {
  static const _channel = MethodChannel('aws.kms.channel');

  Future<void> configure(String keyId, String accessKeyId,
      String secretAccessKey, String region) async {
    try {
      await _channel.invokeMethod<void>(
        'configure',
        {
          'keyId': keyId,
          'accessKeyId': accessKeyId,
          'secretAccessKey': secretAccessKey,
        },
      );
    } catch (e) {
      log('Error during configuration: $e');
    }
  }

  Future<String?> encrypt(String plaintext) async {
    try {
      final encryptedText = await _channel.invokeMethod<String>(
        'encrypt',
        {
          'plaintext': plaintext,
        },
      );
      return encryptedText;
    } catch (e) {
      log('Error during encryption: $e');
      return null;
    }
  }

  Future<String?> decrypt(String encryptedText) async {
    try {
      final decryptedText = await _channel.invokeMethod<String>(
        'decrypt',
        {
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
