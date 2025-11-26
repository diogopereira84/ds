
# Environment Variables:
- Create spring.profiles.active, ADMIN_USER, ADMIN_USER_PASSWORD, API_KEY, and CLIENT_SECRET as an Environment Variables
- Create test_rails_user,test_rails_key, test_rails_project, test_rails_suite as environment variables to allow reporting to test rails


# Run test:
- `mvn clean integration-test -Dspring.profiles.active=<PROFILE> -Dtest=<SPEC>`
- `mvn clean integration-test -Dtest=DataparityAssetCopyAcceptanceSpec`
- `mvn clean integration-test -Dspring.profiles.active=stage -Dtest=DataparityAssetCopyAcceptanceSpec`
- `mvn clean integration-test -Dspring.profiles.active=stage -Dtest=DataparityEDFEventAcceptanceSpec -Dusername=test -Dpasswordd=xxxx -DNMC_PASSPHRASE_DATAPARITY_TEST=xxxx`
- `mvn clean integration-test -Dspring.profiles.active=stage -Dtest=DataparityUpdateAcceptanceSpec -Dusername=test -Dpasswordd=xxxx -DNMC_PASSPHRASE_DATAPARITY_TEST=xxxx`

Where PROFILE is either dev, stage, integ or prod, SPEC is the testing spec need to run. 




