# 화면으로 확인하는 배포와 롤백

이번 실습은 기존 Jenkins Shared Library와 GitOps 저장소를 사용합니다. GitLab CI 연결은 후속 작업입니다. 각 단계에서 직접 결과를 확인한 뒤 다음 단계로 넘어갑니다.

## 1. 로컬에서 화면 확인

서비스 저장소 루트에서 실행합니다.

```sh
./gradlew --no-daemon build
java -Dservice.port=18080 -jar build/libs/service.jar
```

`http://localhost:18080`을 열어 35000원을 계산합니다. 화면에 `shipping-ui-v1`, 무료배송, 합계 35,000원이 나오는지 확인합니다. 다른 프로그램이 18080 포트를 사용 중이면 `service.port`를 18082로 바꾸고 같은 포트로 접속합니다.

## 2. 화면을 포함한 기준 버전 배포

Docker Desktop을 켠 뒤 아래 명령으로 기존 Jenkins 환경을 시작합니다.

```sh
docker start delivery-publish-engine
docker start delivery-jenkins-publish
```

1. `http://localhost:8081`에서 `delivery-github-demo` 잡의 **Build Now**를 누릅니다.
2. `Verify`, `Publish Image`, `Open GitOps PR`이 성공했는지 확인합니다.
3. 아티팩트 `published-image.json`에서 `sourceRevision`이 이번 서비스 커밋인지 확인하고 `image` 값을 기록합니다.
4. GitHub의 `delivery-pipeline-gitops` 저장소에서 생성된 PR을 엽니다. `apps/delivery-demo/deployment.yaml`의 이미지가 같은 digest인지 확인하고 병합합니다.
5. Argo CD의 `delivery-demo`에서 병합 커밋으로 `Synced`, `Healthy`가 되는지 확인합니다. Pod의 `SUMMARY`에서 이미지 digest가 같은지 확인합니다.

Argo CD나 서비스에 접속할 포트가 닫혀 있으면 기존 샌드박스 kubeconfig의 절대경로를 사용합니다. 아래 경로는 이 컴퓨터의 실습 환경 기준입니다. 별도 터미널에서 각각 유지합니다.

```sh
kubectl --kubeconfig /Users/user/Documents/Codex/2026-11-04/cli-readme-ci-check-cli/work/delivery-sandbox/access/kubeconfig -n argocd port-forward svc/argocd-server 18081:443
```

```sh
kubectl --kubeconfig /Users/user/Documents/Codex/2026-11-04/cli-readme-ci-check-cli/work/delivery-sandbox/access/kubeconfig -n delivery-demo port-forward svc/delivery-demo 18080:8080
```

Argo CD는 `https://localhost:18081`, 서비스는 `http://localhost:18080`에서 확인합니다. 로컬 Java 서버가 실행 중이면 먼저 종료해 포트가 겹치지 않게 합니다. 기존 port-forward가 종료되면 다시 실행합니다.

## 3. 새 배송 정책으로 업데이트

1. `src/main/java/example/ShippingQuote.java`의 `FREE_SHIPPING_THRESHOLD`를 `40_000`으로 바꿉니다.
2. `HealthServer.java`의 `shipping-ui-v1`을 `shipping-ui-v2`로 바꿉니다.
3. `ShippingQuoteHttpTest.java`의 버전 응답을 v2와 40000에 맞춥니다. 30000원 요청의 기대 응답은 `shippingFee` 3000, `total` 33000으로 바꾸고, 40000원에서 무료배송인 경계값도 검사합니다.
4. `./gradlew --no-daemon build`를 실행합니다. 성공하면 한국어 커밋을 남기고 push합니다.
5. Jenkins 빌드를 실행하고 새 GitOps PR의 digest를 확인해 병합합니다.
6. Argo CD에서 새 커밋과 Pod를 확인합니다. 서비스 화면을 새로고침해 v2인지 보고 35000원을 입력합니다. 배송비 3,000원, 합계 38,000원이 나와야 합니다.

## 4. GitOps PR로 이전 버전 복원

GitHub에서 **v2 이미지로 교체한 GitOps PR**을 열고 **Revert**로 되돌리는 PR을 만듭니다. GitHub에서 자동 revert가 불가능하면 새 브랜치에서 Deployment의 `image`를 2단계에 기록한 v1 digest로 복원하고 PR을 만듭니다.

변경 diff가 이전 digest를 가리키는지 확인한 뒤 병합합니다. Argo CD는 새 롤백 커밋을 적용합니다. Pod가 준비되면 화면을 새로고침하고 `shipping-ui-v1`, 35000원 주문의 무료배송, 합계 35,000원을 확인합니다. 이미지가 레지스트리에 남아 있어야 복원할 수 있습니다.

## 남길 증거

- Jenkins 빌드 결과와 `published-image.json`
- GitOps 업데이트 PR과 롤백 PR의 이미지 diff
- Argo CD의 적용 커밋, Healthy 상태, Pod 이미지
- 35000원 입력의 v1 → v2 → v1 결과 화면

각 화면의 서비스 버전, 서비스 커밋, GitOps 커밋, 이미지 digest를 함께 기록하면 변경이 어디까지 반영됐는지 추적할 수 있습니다.
