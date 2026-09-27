import { get, post } from "./client";
export const historyApi = {
  list: () => get("/history/get"),
  ingredients: () => get("/history/ingredient/get"),
  preview: (id) => get(`/user/history/${id}/repeat`),
  repeat: (id) => post(`/user/history/${id}/repeat/done`),
};
