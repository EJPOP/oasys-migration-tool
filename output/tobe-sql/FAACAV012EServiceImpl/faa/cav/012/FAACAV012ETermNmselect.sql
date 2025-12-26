/* faa/cav/012/FAACAV012ETermNmselect */ SELECT DECODE(RMRK_CN,'01','월','03','분기','05','연간') AS RMRK_CN /* 비고내용 */,
    RMRK_DTL_CN /* 비고상세내용 */
FROM TWFAACAV024C WHERE ACNTG_SECD = '31' AND ACNTG_CD = #ACNTG_CD#