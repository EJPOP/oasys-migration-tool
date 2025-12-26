SELECT A.LINK_YR /* 연계년도 */,
    A.CRS_CD /* 과정코드 */,
    A.CRS_CDN /* 과정기수 */,
    A.TANE_ID /* 교육생ID */,
    A.FIN_YN /* 수료여부 */,
    A.FNSH_DT /* 수료일시 */
FROM TBCSPSAE301D A WHERE 1=1 AND A.LINK_YR = #LINK_YR# AND A.CRS_CD = #strEDU_CRS_CD# AND A.CRS_CDN = #CRS_CDN# ORDER BY A.LINK_YR, A.CRS_CD, A.CRS_CDN