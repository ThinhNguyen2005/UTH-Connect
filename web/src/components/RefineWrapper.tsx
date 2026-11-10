"use client";

import React, { useState } from "react";
import { Refine } from "@refinedev/core";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { dataProvider } from "@/providers/data-provider";
import { authProvider } from "@/providers/auth-provider";
import { accessControlProvider } from "@/providers/access-control-provider";

export const RefineWrapper: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            refetchOnWindowFocus: false,
            retry: 1,
            staleTime: 30 * 1000,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      <Refine
        dataProvider={dataProvider}
        authProvider={authProvider}
        accessControlProvider={accessControlProvider}
        resources={[
          {
            name: "notifications",
            list: "/notifications",
            create: "/notifications/create",
            show: "/notifications/:id",
            meta: { label: "Thông báo" },
          },
          {
            name: "events",
            list: "/events",
            create: "/events/create",
            show: "/events/:id",
            meta: { label: "Sự kiện" },
          },
          {
            name: "housing_posts",
            list: "/housing",
            create: "/housing/create",
            show: "/housing/:id",
            meta: { label: "Ghép trọ & KTX" },
          },
          {
            name: "profiles",
            list: "/admin/users",
            meta: { label: "Thành viên" },
          },
        ]}
        options={{
          disableTelemetry: true,
        }}
      >
        {children}
      </Refine>
    </QueryClientProvider>
  );
};
