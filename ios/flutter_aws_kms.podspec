Pod::Spec.new do |s|
  s.name             = 'flutter_aws_kms'
  s.version          = '0.0.1'
  s.summary          = 'A new Flutter plugin project.'
  s.description      = <<-DESC
A new Flutter plugin project.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '13.0'

  s.dependency 'AWSKMS'
  s.dependency 'AWSCore'
  s.dependency 'KMSClient'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'

  # Jika membutuhkan resource tambahan
  # s.resource_bundles = {'flutter_aws_kms_privacy' => ['Resources/PrivacyInfo.xcprivacy']}
end
