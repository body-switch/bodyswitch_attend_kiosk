현재 레포(checkin-app)의 API 호출 변경사항을 공유 스펙 문서에 반영합니다.

인자: $ARGUMENTS

## 실행 방법

1. `/mnt/c/Users/dnjs7/work/bodyswitch-api-specs/.claude/commands/sync-spec.md` 파일을 읽어서 전체 동기화 절차를 확인합니다.
2. 현재 레포는 `checkin-app`이므로, 소유 섹션은 **App Usage > Checkin Kiosk (checkin-app)**입니다.
3. 인자로 받은 도메인(또는 자동 감지된 도메인)에 대해 동기화를 실행합니다.

## 소스 파일 위치

- Retrofit API: `app/src/main/java/com/bodyswitch/checkin/data/api/KioskApi.kt`
- DTO: `app/src/main/java/com/bodyswitch/checkin/data/api/dto/`
- Model: `app/src/main/java/com/bodyswitch/checkin/data/model/`

## 공유 스펙 레포

경로: `/mnt/c/Users/dnjs7/work/bodyswitch-api-specs/`
도메인 문서: `domains/{도메인}/{도메인}.md`
