/* faa/cav/024/FAACAV024ECmbBllCdList */ SELECT D.*,
    CD_NM || ' ' || ' - ' || NVL(DAY_DIV2,'월') || '분' AS CD_NM
FROM ( SELECT CD,
    CD ||'.' ||' '|| CD_NM AS CD,
    SUBSTR(RMRK_DTL_CN,0,2) AS SUBSTR,
    REPLACE(SUBSTR(RMRK_DTL_CN,4,5),' ', '') AS REPLACE
FROM ( SELECT ACNTG_CD /* 회계코드 */,
    ACNTG_CDNM /* 회계코드명 */,
    RMRK_DTL_CN AS RMRK_DTL_CN /* 비고상세내용 */
FROM TWFAACAV024C WHERE ACNTG_SECD = '03' AND ACNTG_CD NOT IN ('18','23','24','25','37','38','39','43') ) )D