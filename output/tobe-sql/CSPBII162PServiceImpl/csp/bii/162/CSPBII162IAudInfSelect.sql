SELECT A.INFO_YR /* 정보년도 */,
    A.INFO_SQNO /* 정보순번 */,
    A.INFO_YR || '-' || A.INFO_SQNO AS INFO_YR /* 정보년도 */,
    A.INFO_NM /* 정보명 */
FROM TB_CSPBII100M A /* 감사정보기본 */ WHERE A.INFO_YR = REGEXP_SUBSTR(#strInfNo#, '[^|-]+', 1, 1) AND A.INFO_SQNO = REGEXP_SUBSTR(#strInfNo#, '[^|-]+', 1, 2)