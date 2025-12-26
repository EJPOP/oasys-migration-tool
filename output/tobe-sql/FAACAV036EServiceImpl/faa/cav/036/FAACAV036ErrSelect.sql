/* faa/cav/036/FAACAV036ErrSelect */ SELECT ROWNUM,
    ACNTG_SECD /* 회계구분코드 */,
    ACNTG_CD /* 회계코드 */,
    RMRK_DTL_CN AS RMRK_DTL_CN /* 비고상세내용 */,
    RMRK_CN /* 비고내용 */
FROM TWFAACAV024C WHERE 1=1 AND ACNTG_SECD = #ACNTG_SECD# AND ACNTG_CD &gt;= #startSgCd# ORDER BY ACNTG_CD