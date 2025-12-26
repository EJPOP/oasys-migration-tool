SELECT *
FROM ( SELECT A.INFO_YR /* 정보년도 */,
    A.INFO_SQNO /* 정보순번 */,
    A.INFO_YR || '-' || A.INFO_SQNO AS INFO_YR /* 정보년도 */,
    A.INFO_NM /* 정보명 */,
    TO_CHAR(TO_DATE(A.SBMSN_YMD,'yyyy-MM-dd'),'yyyy-MM-dd') AS SBMSN_YMD /* 제출일자 */,
    F_CODE_NM('1000039', A.PRCS_DRCTN_SECD) AS F_CODE_NM /* 처리유형 */,
    A.ELY_SBMSN_ADD_SCR /* 조기제출추가점수 */,
    CASE WHEN substr(A.SBMSN_YMD,5) BETWEEN '0101' AND '0331' THEN '1' WHEN substr(A.SBMSN_YMD,5) BETWEEN '0401' AND '0630' THEN '2' WHEN substr(A.SBMSN_YMD,5) BETWEEN '0701' AND '0930' THEN '3' ELSE '4' END AS SUBSTR,
    CASE WHEN substr(to_char(sysdate,'yyyyMMdd'),5) BETWEEN '0101' AND '0331' THEN '1' WHEN substr(to_char(sysdate,'yyyyMMdd'),5) BETWEEN '0401' AND '0630' THEN '2' WHEN substr(to_char(sysdate,'yyyyMMdd'),5) BETWEEN '0701' AND '0930' THEN '3' ELSE '4' END AS SUBSTR
FROM TBCSPBII170M A /* 감사정보기본 */ WHERE 1=1 AND A.INFO_YR = #INFO_YR# AND A.PRSNR_ENO = #PRSNR_ENO# ) WHERE CHK_DAY = TO_DAY