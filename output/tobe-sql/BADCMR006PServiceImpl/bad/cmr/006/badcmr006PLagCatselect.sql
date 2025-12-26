SELECT A.INFO_YR /* 정보년도 */,
    A.LAG_CAT_SNO /* 대분류번호 */,
    A.LAG_CAT_TIT_NM /* 대분류제목 */,
    A.LAG_CAT_TXT,
    A.REG_YMD /* 등록일자 */,
    A.RGTR_ID /* 등록자아이디 */,
    A.MDFCN_YMD /* 수정일자 */,
    A.MDFCN_PIC_ID /* 수정담당자아이디 */,
    A.DEL_YN /* 삭제여부 */
FROM TBBADCMR001M A WHERE 1 = 1 AND A.LAG_CAT_SNO = #strLagCatSno# /* 대분류번호 */ AND A.LAG_CAT_TIT_NM LIKE #strLagCatTitNm#||'%' /* 대분류제목 */ AND A.INFO_YR = #INFO_YR# ORDER BY INFO_YR, LAG_CAT_SNO