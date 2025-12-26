SELECT LINK_YR AS LINK_YR /* 연계년도 */,
    #N/A /* 조직인원수1 */,
    #N/A /* 조직인원수2 */,
    #N/A /* 조직인원수3 */,
    #N/A /* 조직인원수4 */,
    #N/A /* 조직인원수5 */,
    #N/A /* 공무원인원수1 */,
    #N/A /* 공무원인원수2 */,
    #N/A /* 세출예산금액1 */,
    #N/A /* 세출예산금액2 */,
    FNA_SRA_RT /* 재정자립비율 */,
    FNA_SRA_DTL_RT /* 재정자립상세비율 */
FROM TBFAAAEA024L WHERE ORG_CD = #strDetailOrgCd# WHERE INST_CD LIKE REPLACE(#INST_CD#,'000','') || '%' AND DEL_YN = 'N' ORDER BY LINK_YR DESC