SELECT TOT1 + TOT2 AS TOT1 /* 이송접수 총합계 */,
    TOT1 /* 금년신규 */,
    TOT2 /* 전년이월 */,
    TOT3 + TOT4 + TOT5 + TOT6 AS TOT3 /* 처리완료 총합계 */,
    TOT3 /* 입건 */,
    TOT4 /* 유용 */,
    TOT5 /* 불용 */,
    TOT6 /* 종결 */,
    TOT7 /* 미결 */
FROM ( SELECT (SELECT count(*)
FROM TBCSPBII170M WHERE PRCS_DRCTN_SECD = '6' AND TRNF_YMD BETWEEN #strTrnfStrDt# AND #strTrnfEndDt# AND DSPS_RQT_NM LIKE '%' || #strSrhTxt# || '%' AND ETC_LBSVC_MNG_DEPT_CD LIKE NVL(SUBSTR(#strHndlDptCd#, 0, INSTR(#strHndlDptCd#, '0000') -1), #strHndlDptCd#) || '%' ) AS TOT1 , (SELECT count(*)
FROM TBCSPBII170M WHERE PRCS_DRCTN_SECD = '6' AND TRNF_YMD AND DSPS_RQT_NM LIKE '%' || #strSrhTxt# || '%' AND ETC_LBSVC_MNG_DEPT_CD LIKE NVL(SUBSTR(#strHndlDptCd#, 0, INSTR(#strHndlDptCd#, '0000') -1), #strHndlDptCd#) || '%' ) AS TOT2 , (SELECT count(*)
FROM TBCSPBII170M WHERE PRCS_DRCTN_SECD = '6' AND TRNF_YMD BETWEEN #strTrnfStrDt# AND #strTrnfEndDt# AND PRCS_RSLT_SECD IN ('31','32') AND DSPS_RQT_NM LIKE '%' || #strSrhTxt# || '%' AND ETC_LBSVC_MNG_DEPT_CD LIKE NVL(SUBSTR(#strHndlDptCd#, 0, INSTR(#strHndlDptCd#, '0000') -1), #strHndlDptCd#) || '%' ) AS TOT3 , (SELECT count(*)
FROM TBCSPBII170M WHERE PRCS_DRCTN_SECD = '6' AND TRNF_YMD BETWEEN #strTrnfStrDt# AND #strTrnfEndDt# AND PRCS_RSLT_SECD = '35' AND DSPS_RQT_NM LIKE '%' || #strSrhTxt# || '%' AND ETC_LBSVC_MNG_DEPT_CD LIKE NVL(SUBSTR(#strHndlDptCd#, 0, INSTR(#strHndlDptCd#, '0000') -1), #strHndlDptCd#) || '%' ) AS TOT4 , (SELECT count(*)
FROM TBCSPBII170M WHERE PRCS_DRCTN_SECD = '6' AND TRNF_YMD BETWEEN #strTrnfStrDt# AND #strTrnfEndDt# AND PRCS_RSLT_SECD = '36' AND DSPS_RQT_NM LIKE '%' || #strSrhTxt# || '%' AND ETC_LBSVC_MNG_DEPT_CD LIKE NVL(SUBSTR(#strHndlDptCd#, 0, INSTR(#strHndlDptCd#, '0000') -1), #strHndlDptCd#) || '%' ) AS TOT5 , (SELECT count(*)
FROM TBCSPBII170M WHERE PRCS_DRCTN_SECD = '6' AND TRNF_YMD BETWEEN #strTrnfStrDt# AND #strTrnfEndDt# AND PRCS_RSLT_SECD = '88' AND DSPS_RQT_NM LIKE '%' || #strSrhTxt# || '%' AND ETC_LBSVC_MNG_DEPT_CD LIKE NVL(SUBSTR(#strHndlDptCd#, 0, INSTR(#strHndlDptCd#, '0000') -1), #strHndlDptCd#) || '%' ) AS TOT6 , (SELECT count(*)
FROM TBCSPBII170M WHERE PRCS_DRCTN_SECD = '6' AND TRNF_YMD BETWEEN #strTrnfStrDt# AND #strTrnfEndDt# AND PRCS_RSLT_SECD IS NULL AND DSPS_RQT_NM LIKE '%' || #strSrhTxt# || '%' AND ETC_LBSVC_MNG_DEPT_CD LIKE NVL(SUBSTR(#strHndlDptCd#, 0, INSTR(#strHndlDptCd#, '0000') -1), #strHndlDptCd#) || '%' ) AS TOT7 FROM DUAL )