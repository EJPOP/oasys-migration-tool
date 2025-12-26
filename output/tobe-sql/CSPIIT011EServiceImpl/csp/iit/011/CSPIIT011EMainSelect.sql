SELECT A.PC_EQP_CD,
    /*코드*/ A.PC_EQP_NM,
    /*코드명*/ A.USE_DVSN_CD,
    /*사용여부*/ A.REG_YMD AS REG_YMD /* 등록일자 */,
    /*입력일*/ A.ABRG_DT,
    /*폐기일*/ A.PC_EQP_CD_RMK,
    /*비고*/ A.PC_KND_CD /* PC장비코드 */
FROM TBCSPIIT011C A /*PC장비코드*/ WHERE 1=1 AND A.USE_DVSN_CD = #strUSE_DVSN_CD# AND A.PC_EQP_CD = #strPC_EQP_CD# AND A.PC_EQP_NM LIKE '%'||#strPC_EQP_NM#||'%' ORDER BY A.PC_KND_CD ,A.PC_EQP_CD