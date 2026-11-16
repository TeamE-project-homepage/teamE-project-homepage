# BinGo 협업 가이드

## 브랜치 운영

- `main`에는 팀이 통합 테스트를 마친 버전을 반영합니다.
- `develop`은 팀원들의 작업을 모아 테스트하는 브랜치입니다.
- 기능 개발, 버그 수정, 문서 작업은 최신 `develop`에서 작업 브랜치를 만들어 진행합니다.
- 작업 브랜치의 변경은 PR을 통해 `develop`에 반영합니다. 테스트를 마친 버전은 `develop`에서 `main`으로 PR을 올립니다.

## 작업 시작하기

기능 개발이나 버그 수정에 관련된 GitHub Issue가 있으면 담당자와 완료 조건을 확인합니다. 아래는 자치구 선택 기능을 시작하는 예시입니다.

```bash
git fetch origin
git switch --no-track -c feature/district-selection origin/develop
```

| 작업 유형 | 브랜치 이름 예시             |
| --------- | ---------------------------- |
| 기능 개발 | `feature/district-selection` |
| 버그 수정 | `fix/photo-upload`           |
| 문서 작업 | `docs/repository-guide`      |

## 커밋과 PR 제목

커밋 메시지와 PR 제목은 `type: short description` 형식을 사용합니다. 유형은 소문자로 쓰고, 설명은 무엇을 바꿨는지 영어 동사로 시작합니다.

- `feat: add Seoul district selector`
- `fix: handle empty search results`
- `docs: add collaboration guide`
- `chore: initialize BinGo project`

커밋 하나에는 한 가지 목적의 변경을 담습니다. 같은 기능이라도 화면 추가와 동작 연결을 각각 확인할 수 있다면 커밋을 나눕니다. PR 제목은 PR 전체의 변경을 요약합니다.

## PR 작성과 리뷰

1. 일반 작업 PR을 열 때는 `base`를 `develop`으로 선택합니다.
2. 기존 PR 템플릿의 `Summary`, `Changes`, `Testing`, `Review` 항목을 채웁니다.
3. `Testing`에는 실제로 확인한 내용이나 리뷰어가 변경을 확인할 방법을 적습니다. UI가 바뀌었다면 스크린샷을 첨부합니다.
4. 관련 Issue가 있으면 본문에 `Related to #12`처럼 번호를 적습니다.
5. 팀원 2명 이상의 승인을 받은 뒤 병합합니다.
