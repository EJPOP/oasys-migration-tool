/* faa/cav/054/FAACAV054EMainselect */ SELECT PRCS_ID /* 처리아이디 */,
    PRCS_STTS_ID /* 처리상태아이디 */,
    DECODE(PRCS_STTS_SECD,'W','작업중') AS PRCS_STTS_SECD /* 처리상태구분코드 */,
    PRCS_STTS_SECD /* 처리상태구분코드 */,
    BTCH_SATU_DT /* 배치기동일시 */,
    DECODE(END_YMD,'W','') AS END_YMD /* 종료일시 */
FROM TWFAACAV052L