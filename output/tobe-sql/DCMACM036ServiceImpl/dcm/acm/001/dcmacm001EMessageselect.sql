SELECT MSG_CD /* 메시지코드 */,
    MSG_NM /* 메시지명 */,
    TASK_SECD /* 업무구분코드 */,
    FUNC_CAT_CD||TASK_SECD AS FUNC_CAT_CD /* 기능분류코드 */,
    MSG_CD
FROM TBDCMACM012C WHERE 1=1 MSG_NM LIKE '%'||#strMsgNm# ||'%' TASK_SECD LIKE '%'||#strTskCatCd# ||'%' FUNC_CAT_CD LIKE '%'||#strFuncCatCd# ||'%' ORDER BY MSG_CD