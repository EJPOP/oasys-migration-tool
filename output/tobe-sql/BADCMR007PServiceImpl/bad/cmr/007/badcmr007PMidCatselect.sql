SELECT A.INFO_YR /* 정보년도 */,
    A.LAG_CAT_SNO,
    B.LAG_CAT_TIT_NM,
    A.MID_CAT_SNO,
    A.MID_CAT_TIT_NM,
    A.MID_CAT_TXT,
    A.CHR_DPT_CD,
    A.REG_YMD /* 등록일자 */,
    A.RGTR_ID /* 등록자아이디 */,
    A.MDFCN_YMD /* 수정일자 */,
    A.MDFCN_PIC_ID /* 수정담당자아이디 */,
    A.DEL_YN /* 삭제여부 */
FROM TBBADCMR002M A, TBBADCMR001M B WHERE A.LAG_CAT_SNO = B.LAG_CAT_SNO AND A.INFO_YR = B.INFO_YR AND A.MID_CAT_SNO = #strMidCatSno# /* 중분류번호 */ AND A.MID_CAT_TIT_NM LIKE '%'||#strMidCatTitNm#||'%' /* 중분류제목 */ AND A.LAG_CAT_SNO =#strLagCatSno# AND A.INFO_YR =#INFO_YR# ORDER BY INFO_YR, LAG_CAT_SNO