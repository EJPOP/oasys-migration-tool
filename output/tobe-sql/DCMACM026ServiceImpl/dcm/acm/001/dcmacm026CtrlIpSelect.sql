SELECT CTRL_NO /* 통제번호 */,
    SIT_CD /* 사이트코드 */,
    STR_IP_AD /* 시작ip */,
    END_IP_AD /* 종료 ip */,
    CNCT_PMS_STR_DH,
    CNCT_PMS_END_DH,
    SUBSTR(TO_CHAR(CNCT_PMS_STR_DH,'yyyymmddhh24miss'),9,14) AS SUBSTR /* 시작일 */,
    SUBSTR(TO_CHAR(CNCT_PMS_END_DH,'yyyymmddhh24miss'),9,14) AS SUBSTR /* 종료일 */
FROM TBDCMACM027L WHERE 1=1 AND SIT_CD = #strSitCd#