SELECT A.APPROVAL_APPROVALDATE /* 결재일 */,
    A.DOCNO /* 문서번호 */,
    A.DOCTITLE /* 문서제목 */,
    A.REPORT_USERNAME /* 보고자명 */,
    A.APPROVAL_USERNAME /* 결재자명 */,
    A.REQUESTID /* 요청ID */,
    A.ADT_RPTP_EDT_HSTRY_SQNO /* 감사보고서편집이력순번 */,
    A.DOCID /* 문서관리카드ID */
FROM AM_ONNARA_RESPONSE_DOC A WHERE A.REQUESTID = #strRequestId# AND A.ADT_RPTP_EDT_HSTRY_SQNO = convert(numeric, #strSeq#) ORDER BY A.DOCID