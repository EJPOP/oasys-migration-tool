SELECT DST_NM /* 출발역명 */,
    ARVL_CRTR_DST_NM /* 도착기준출발역명 */,
    CRG_SECD /* 요금구분코드 */,
    TFE_KDCD /* 교통편종류코드 */,
    TFE_KDCD /* 교통편종류코드 */,
    DSCNT_RT /* 할인비율 */,
    CRG_AMT /* 요금금액 */,
    DSCNT_BFR_AMT /* 할인이전금액 */,
    CRG_CRTR_CN /* 요금기준내용 */,
    MDFCN_YMD /* 수정일자 */,
    CPRM_AMT /* 특별실금액 */,
    BZTRP_SECD /* 출장구분코드 */
FROM TB_BADDED016B WHERE 1=1 AND DST_NM LIKE #strDepStadStnNm#||'%' /*출발역*/ AND ARVL_CRTR_DST_NM LIKE #strArvStadStnNm#||'%' /*도착역*/ AND TFE_KDCD = #TFE_KDCD# /*교통편*/ AND BZTRP_SECD = #BZTRP_SECD# ORDER BY DST_NM, ARVL_CRTR_DST_NM