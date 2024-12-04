import 'dart:developer';
import 'dart:io';

import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter_aws_kms/flutter_aws_kms.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AWS KMS Demo',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const KmsDemoPage(),
    );
  }
}

class KmsDemoPage extends StatefulWidget {
  const KmsDemoPage({super.key});

  @override
  _KmsDemoPageState createState() => _KmsDemoPageState();
}

class _KmsDemoPageState extends State<KmsDemoPage> {
  final AwsKmsService _awsKmsService = AwsKmsService();
  final TextEditingController _plaintextController = TextEditingController();

  final String _accessKeyId = '';
  final String _secretAccessKey = '';
  final String _region = '';
  final String _arnKey =
      '';

  String? _encryptedText;
  String? _decryptedText;

  @override
  void initState() {
    _initialize();
    super.initState();
  }

  void _initialize() async {
    try {
      await _awsKmsService.configure(_arnKey, _accessKeyId, _secretAccessKey);
      log('AWS KMS configured successfully');
    } catch (e) {
      log('Error during initialization: $e');
    }
  }

  Future<void> _encryptText() async {
    final plaintext = _plaintextController.text;

    if (plaintext.isNotEmpty) {
      final encrypted = await _awsKmsService.encrypt(
        plaintext,
      );
      log('Encrypted: $encrypted');
      setState(() {
        _encryptedText = encrypted;
      });
    }
  }

  Future<void> _decryptText() async {
    if (_encryptedText != null) {
      final decrypted = await _awsKmsService.decrypt(
        _encryptedText!,
      );
      log('Decrypted: $decrypted');
      setState(() {
        _decryptedText = decrypted;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('AWS KMS Demo')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: _plaintextController,
              decoration: const InputDecoration(labelText: 'Plaintext'),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _encryptText,
              child: const Text('Encrypt'),
            ),
            if (_encryptedText != null) ...[
              const SizedBox(height: 20),
              const Text('Encrypted Text:'),
              SelectableText(_encryptedText ?? ''),
            ],
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _decryptText,
              child: const Text('Decrypt'),
            ),
            if (_decryptedText != null) ...[
              const SizedBox(height: 20),
              const Text('Decrypted Text:'),
              SelectableText(_decryptedText ?? ''),
            ],
          ],
        ),
      ),
    );
  }
}
