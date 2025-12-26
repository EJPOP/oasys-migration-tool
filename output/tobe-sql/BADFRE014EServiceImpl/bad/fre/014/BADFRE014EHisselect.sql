SELECT CVLCPT_ADT_YR /* 민원감사년도 */,
    ADT_NO /* 감사번호 */,
    DSPS_RQT_SQNO /* 처분요구순번 */,
    SRECS_REL_INST_CD /* 심사재심의관계기관코드 */,
    F_ORG_INFO(SRECS_REL_INST_CD,'1') AS REL_INST_NM /* 관계기관명 */,
    MNG_SQNO /* 관리순번 */,
    SPCL_MNG_IMPL_SQNO /* 특수관리집행순번 */,
    IMPL_YMD /* 집행일자 */,
    IMPL_AMT /* 집행금액 */,
    SRECS_IMPL_CN /* 심사재심의집행내용 */,
    CFMTN_YN /* 확정여부 */
FROM TB_BADFRE011L WHERE 1=1 AND CVLCPT_ADT_YR = #CVLCPT_ADT_YR# /* 감사년도 */ AND ADT_NO = #ADT_NO# /* 감사번호 */ AND DSPS_RQT_SQNO = #DSPS_RQT_SQNO# /* 처분요구순번 */ AND SRECS_REL_INST_CD = #SRECS_REL_INST_CD# /* 관계기관코드 */ AND MNG_SQNO = #MNG_SQNO# /* 관리순번 */ ORDER BY SPCL_MNG_IMPL_SQNO DESC