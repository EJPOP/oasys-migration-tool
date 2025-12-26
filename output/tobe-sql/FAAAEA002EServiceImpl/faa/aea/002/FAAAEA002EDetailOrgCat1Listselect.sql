SELECT LINK_YR AS LINK_YR /* 연계년도 */,
    #N/A /* 조직인원수1 */,
    #N/A /* 조직인원수2 */,
    #N/A /* 조직인원수3 */,
    #N/A /* 조직인원수4 */,
    #N/A /* 공무원인원수1 */,
    #N/A /* 공무원인원수2 */,
    #N/A /* 공무원인원수3 */,
    #N/A /* 세출예산금액1 */
FROM TBFAAAEA024L WHERE INST_CD = #INST_CD# WHERE INST_CD LIKE REPLACE(#INST_CD#,'000','') || '%' AND DEL_YN = 'N' ORDER BY CVLCPT_ADT_YR DESC