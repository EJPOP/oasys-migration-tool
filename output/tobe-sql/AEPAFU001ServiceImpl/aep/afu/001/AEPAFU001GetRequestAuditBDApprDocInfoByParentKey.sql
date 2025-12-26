SELECT A.SRECS_RCPT_STP_SECD AS SRECS_RCPT_STP_SECD /* 심사재심의접수단계구분코드 */,
    NVL(B.SRECS_RCPT_STP_NM,'') AS ADT_STP_NM /* 감사단계명 */,
    A.SRECS_RPTP_NM /* 심사재심의보고서명 */,
    A.SRECS_RPTP_KDCD /* 심사재심의보고서종류코드 */,
    A.SRECS_ATRZ_DMND_ID /* 심사재심의결재요청아이디 */,
    A.SRECS_RCPT_YR AS SRECS_RCPT_YR /* 심사재심의접수년도 */,
    A.SRECS_RCPT_SQNO AS SRECS_RCPT_SQNO /* 심사재심의접수순번 */,
    A.SRECS_DOC_ID /* 심사재심의문서아이디 */,
    A.SRECS_ATRZ_STTS_SECD /* 심사재심의결재상태구분코드 */,
    A.SRECS_RCPT_SECD AS SRECS_RCPT_SECD /* 심사재심의접수구분코드 */,
    F_OPEN_EDIT(A.SRECS_LNKG_URL_ADDR) AS F_OPEN_EDIT
FROM TB_BADFRE103M A LEFT JOIN TB_BADFRE101M B ON A.SRECS_RCPT_YR = B.SRECS_RCPT_YR AND A.SRECS_RCPT_SECD = B.SRECS_RCPT_SECD AND A.SRECS_RCPT_SQNO = B.SRECS_RCPT_SQNO AND A.SRECS_RCPT_STP_SECD = B.SRECS_RCPT_STP_SECD WHERE 1=1 AND A.SRECS_ATRZ_DMND_ID IS NULL (#docInfoList[].audYr#,#docInfoList[].reqDvsnCd#,#docInfoList[].audNo#,#docInfoList[].audStepCd#,#docInfoList[].docId#)