import { get, post, put, del } from "./client";
export const pantryApi = {
  list: () => get("/pantry/get"),
  ingredients: () => get("/ingredient/get"),
  add: (d) => post("/pantry/add", d),
  edit: (id, d) => put(`/pantry/update/${id}`, d),
  remove: (id) => del(`/pantry/delete/${id}`),
  low: () => get("/user/low"),
  email: () => post("/user/low/email"),
};
