import { get, post, put, del } from "./client";
export const adminApi = {
  overview: () => get("/admin/overview"),
  users: () => get("/user/get"),
  removeUser: (id) => del(`/user/delete/${id}`),
  addIngredient: (d) => post("/ingredient/add", d),
  editIngredient: (id, d) => put(`/ingredient/update/${id}`, d),
  removeIngredient: (id) => del(`/ingredient/delete/${id}`),
};
