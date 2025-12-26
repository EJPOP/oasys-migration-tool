SELECT DECODE(#strHndlStaDvsnCd#, 0, '전체', F_CODE_NM ('1000587', #strHndlStaDvsnCd#)) AS #STRHNDLSTADVSNCD# /* 처리구분 */,
    F_CODE_NM('1000200', #PRCS_OPNN_SECD#) AS F_CODE_NM /* 처리결과 */,
    F_CODE_NM('1000200', #PRCS_OPNN_SECD#) || ', ' || F_CODE_NM('1000200', #strHndlOpinCd2#) AS F_CODE_NM /* 처리결과 */,
    F_CODE_NM('1000200', #PRCS_OPNN_SECD#) || ', ' || F_CODE_NM('1000200', #strHndlOpinCd2#) || ', ' || F_CODE_NM('1000200', #strHndlOpinCd3#) AS F_CODE_NM /* 처리결과 */,
    F_CODE_NM('1000200', #PRCS_OPNN_SECD#) || ', ' || F_CODE_NM('1000200', #strHndlOpinCd2#) || ', ' || F_CODE_NM('1000200', #strHndlOpinCd3#) || ', ' || F_CODE_NM('1000200', #strHndlOpinCd4#) AS F_CODE_NM /* 처리결과 */,
    F_CODE_NM('1000200', #PRCS_OPNN_SECD#) || ', ' || F_CODE_NM('1000200', #strHndlOpinCd2#) || ', ' || F_CODE_NM('1000200', #strHndlOpinCd3#) || ', ' || F_CODE_NM('1000200', #strHndlOpinCd4#) || ', ' || F_CODE_NM('1000200', #strHndlOpinCd5#) AS F_CODE_NM /* 처리결과 */,
    '' AS CND_HNDL_OPIN_NM /* 처리결과 */,
    F_ORG_INFO(#INST_CD#,'1') AS F_ORG_INFO /* 관계기관 */,
    #strSchTxt# AS #STRSCHTXT# /* 검색어 */,
    F_ORG_INFO(INST_CD,'1') || '-' || LINK_YR || '-' || FRS_MDM_SQNO AS F_ORG_INFO /* 통보번호 */,
    F_CODE_NM('1000652', STT_NTFCTN_SECD) AS F_CODE_NM /* 법령통보구분명 */,
    CASE WHEN REL_STT_NTFCTN_SECD IS NOT NULL THEN F_CODE_NM('1000653', REL_STT_NTFCTN_SECD) ELSE CTRT_BSS_CN END AS REL_STT_NTFCTN_SECD /* 관계법령통보구분코드 */,
    NTFCTN_NM /* 통보명 */,
    CASE WHEN STT_RCPT_STTS_SECD IN ('58','59') THEN '미접수' WHEN STT_RCPT_STTS_SECD IN ('71','72','76') THEN '처리중' WHEN STT_RCPT_STTS_SECD = '75' THEN '결재중' WHEN STT_RCPT_STTS_SECD = '77' THEN '완결' ELSE STT_RCPT_STTS_SECD END AS STT_RCPT_STTS_SECD /* 법령접수상태구분코드 */,
    CASE WHEN PRCS_OPNN_SECD IS NOT NULL THEN F_CODE_NM('1000200', PRCS_OPNN_SECD) ELSE '미처리' END AS PRCS_OPNN_SECD /* 처리의견구분코드 */,
    TO_CHAR(TO_DATE(CPLT_YMD),'YYYY.MM.DD') AS CPLT_YMD /* 완결일자 */,
    TO_CHAR(TO_DATE(NTFCTN_RCPT_YMD),'YYYY.MM.DD') AS NTFCTN_RCPT_YMD /* 통보접수일자 */
FROM TB_BADGDA004M /* 관련법령통지통보 */ WHERE 1=1 AND STT_RCPT_STTS_SECD != '51' AND SRECS_DEPT_CD IN (SELECT SRECS_DEPT_CD /* 심사재심의부서코드 */
FROM TABLE(F_AUTH_DEPT(#strCnb#, #strDptAuth#, '', #strDptCd#))) AND SRECS_DEPT_CD = #SRECS_DEPT_CD# AND NTFCTN_RCPT_YMD BETWEEN #strAcpDtStr# AND #strAcpDtEnd# /* 접수일자 */ AND STT_RCPT_STTS_SECD IN ('58', '59', '71', '72', '75', '76') AND STT_RCPT_STTS_SECD IN ('71', '72', '76') AND STT_RCPT_STTS_SECD = '75' AND STT_RCPT_STTS_SECD = '77' AND STT_NTFCTN_SECD = #STT_NTFCTN_SECD# AND INST_CD = #INST_CD# AND PRCS_OPNN_SECD = #PRCS_OPNN_SECD# AND PRCS_OPNN_SECD IN (#PRCS_OPNN_SECD#, #strHndlOpinCd2#) AND PRCS_OPNN_SECD IN (#PRCS_OPNN_SECD#, #strHndlOpinCd2#, #strHndlOpinCd3#) AND PRCS_OPNN_SECD IN (#PRCS_OPNN_SECD#, #strHndlOpinCd2#, #strHndlOpinCd3#, #strHndlOpinCd4#) AND PRCS_OPNN_SECD IN (#PRCS_OPNN_SECD#, #strHndlOpinCd2#, #strHndlOpinCd3#, #strHndlOpinCd4#, #strHndlOpinCd5#) AND (NTFCTN_NM LIKE '%' || #strSchTxt# || '%' OR NTFCTN_CN LIKE '%' || #strSchTxt# || '%') AND NTFCTN_NM LIKE '%' || #strSchTxt# || '%' AND NTFCTN_CN LIKE '%' || #strSchTxt# || '%' ORDER BY NTFCTN_RCPT_YMD DESC ,INST_CD ,LINK_YR DESC ,FRS_MDM_SQNO DESC